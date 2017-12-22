// Copyright 2017 Imperial College London
//
// Permission is hereby granted, free of charge, to any person obtaining
// a copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to
// permit persons to whom the Software is furnished to do so, subject to
// the following conditions:
//
// The above copyright notice and this permission notice shall be
// included in all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
// EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
// LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
// WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package uk.ac.ic.doc.multicore.oglfuzzer.server;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.ExecHelper;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.ExecHelper.RedirectType;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.ExecResult;
import uk.ac.ic.doc.multicore.oglfuzzer.common.util.ToolPaths;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.CommandInfo;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.CommandResult;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.FuzzerServiceManager;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ImageJob;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.Job;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.JobInfo;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.ServerInfo;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.Token;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.TokenNotFoundException;
import uk.ac.ic.doc.multicore.oglfuzzer.server.thrift.WorkerInfo;

public class FuzzerServiceManagerImpl implements FuzzerServiceManager.Iface {

  private static final Logger LOGGER = LoggerFactory.getLogger(FuzzerServiceManagerImpl.class);

  private FuzzerServiceImpl service;

  private final AtomicLong jobIdCounter;

  private final ICommandDispatcher commandDispatcher;

  public FuzzerServiceManagerImpl(FuzzerServiceImpl service,
        ICommandDispatcher commandDispatcher) {
    this.service = service;
    this.jobIdCounter = new AtomicLong();
    this.commandDispatcher = commandDispatcher;
  }

  @Override
  public void clearClientJobQueue(Token forClient) throws TException {
    try {
      service.getClientWorkQueue(forClient).clearQueue();
    } catch (InterruptedException exception) {
      throw new TException(exception);
    }
  }

  @Override
  public Job submitJob(Job job, Token forClient, int retryLimit) throws TException {
    LOGGER.info("submitJob {}", forClient);
    Job[] result = new Job[1];

    if (!service.getSessionMap().containsToken(forClient)) {
      throw new TokenNotFoundException().setToken(forClient);
    }

    service.getSessionMap().lockSessionAndExecute(forClient, session -> {
      session.jobQueue.add(new SingleJob(job, job1 -> {
        synchronized (result) {
          result[0] = job1;
          result.notifyAll();
        }
      }, jobIdCounter, retryLimit));
      return null;
    });

    synchronized (result) {
      while (result[0] == null) {
        try {
          result.wait();
        } catch (InterruptedException exception) {
          throw new TException(exception);
        }
      }
    }

    return result[0];
  }

  @Override
  public void queueCommand(
        String name,
        List<String> command,
        String queueName,
        String logFile)
        throws TException {

    if (name == null) {
      throw new TException("name must be set.");
    }

    if (command == null || command.isEmpty()) {
      throw new TException("Command must be a non-empty list.");
    }

    if (queueName == null) {
      throw new TException("queueName must be set.");
    }

    final Token token = new Token().setValue(queueName);

    if (!service.getSessionMap().containsToken(token)) {
      throw new TokenNotFoundException().setToken(token);
    }

    try {
      Path workDir = Paths.get(".").toAbsolutePath().normalize();

      if (logFile != null) {
        Path child = Paths.get(logFile).toAbsolutePath().normalize();
        if (!child.startsWith(workDir)) {
          throw new TException("Invalid log file location.");
        }
      }

      service.getSessionMap().lockSessionAndExecute(
          token, session -> {
            session.workQueue.add(new CommandRunnable(
                name,
                command,
                queueName,
                logFile,
                this,
                commandDispatcher));
            return null;
          });
    } catch (Exception ex) {
      LOGGER.error("", ex);
      throw new TException(ex);
    }
  }

  @Override
  public CommandResult executeCommand(String name, List<String> command) throws TException {
    try {
      ExecResult res =
            new ExecHelper(ToolPaths.getPythonDriversDir()).exec(
                  RedirectType.TO_BUFFER,
                  null,
                  true,
                  command.toArray(new String[0])
            );

      return new CommandResult().setOutput(res.stdout.toString())
            .setError(res.stderr.toString())
            .setExitCode(res.res);
    } catch (Exception ex) {
      throw new TException(ex);
    }
  }

  @Override
  public ServerInfo getServerState() throws TException {

    // Get reduction queue.
    List<CommandInfo> reductionQueue = new ArrayList<>();

    {
      List<String> reductionQueueStr = service.getReductionWorkQueue().queueToStringList();
      for (String reductionCommand : reductionQueueStr) {
        reductionQueue.add(new CommandInfo().setName(reductionCommand));
      }
    }

    // Get workers
    List<WorkerInfo> workers = new ArrayList<>();

    {
      Set<Token> tokens = service.getSessionMap().getTokenSet();
      for (Token token : tokens) {
        service.getSessionMap().lockSessionAndExecute(token, session -> {

          workers.add(
                new WorkerInfo()
                      .setToken(token.getValue())
                      .setCommandQueue(session.workQueue.getQueueAsCommandInfoList())
                      .setJobQueue(getJobQueueAsJobInfoList(session.jobQueue))
                      .setLive(session.isLive())
          );

          return null;
        });
      }
    }

    return
          new ServerInfo()
                .setReductionQueue(reductionQueue)
                .setWorkers(workers);
  }

  private List<JobInfo> getJobQueueAsJobInfoList(Queue<IServerJob> jobQueue) {
    List<JobInfo> res = new ArrayList<>();
    for (IServerJob job : jobQueue) {
      if (job instanceof SingleJob) {
        SingleJob sj = (SingleJob) job;
        if (sj.job.isSetImageJob()) {
          ImageJob ij = sj.job.getImageJob();

          StringBuilder infoString = new StringBuilder();
          if (ij.isSetShader() && ij.getShader().isSetName()) {
            infoString.append(ij.getShader().getName());
          }
          if (ij.isSetMeta()) {
            infoString.append(ij.getMeta());
            infoString.append("; ");
          }
          res.add(new JobInfo().setInfo(infoString.toString()));
          continue;
        }
      }
      // otherwise:
      res.add(new JobInfo().setInfo(job.toString()));
    }
    return res;
  }

}
