

// See: https://wiki.apache.org/thrift/Tutorial/

namespace java uk.ac.ic.doc.multicore.oglfuzzer.server.thrift
namespace js oglfuzzerserver
namespace py oglfuzzerserver
namespace cocoa oglfuzzerserver
namespace cpp oglfuzzerserver
//namespace swift oglfuzzerserver
//namespace php oglfuzzerserver
//namespace perl oglfuzzerserver
//namespace d oglfuzzerserver
//namespace dart oglfuzzerserver
//namespace haxe oglfuzzerserver

const string IDENTIFIER_DESKTOP = "desktop";
const string IDENTIFIER_ANDROID = "android";
const string IDENTIFIER_IOS = "ios";

const string UPLOAD_FIELD_NAME_FILE = "file";
const string UPLOAD_FIELD_NAME_TOKEN = "token";
const string UPLOAD_FIELD_NAME_ID = "id"; // the shader set id

const string DOWNLOAD_FIELD_NAME_TOKEN = UPLOAD_FIELD_NAME_TOKEN;

// get_image exit codes.
const i32 COMPILE_ERROR_EXIT_CODE = 101;
const i32 LINK_ERROR_EXIT_CODE = 102;
const i32 RENDER_ERROR_EXIT_CODE = 103;

enum ResultConstant {
    ERROR,
    COMPILE_ERROR,
    LINK_ERROR,
    NONDET,
    TIMEOUT,
    UNEXPECTED_ERROR,
    SKIPPED,
}

enum ReductionKind {
    IDENTICAL,
    NOT_IDENTICAL,
    ABOVE_THRESHOLD,
    BELOW_THRESHOLD,
    ERROR, // given a ResultConstant or some other regex
    VALIDATOR_ERROR
}

enum TokenError {
  SERVER_ERROR = 0,
  INVALID_PLATFORM_INFO,
  INVALID_PROVIDED_TOKEN,
  PLATFORM_INFO_CHANGED,
}

struct JobInfo {
  1 : optional string info
}

struct CommandInfo {
  1 : optional string name,
  2 : optional list<string> command,
  3 : optional string logFile,
}

struct WorkerInfo {
  1 : optional string token,
  2 : optional list<CommandInfo> commandQueue,
  3 : optional list<JobInfo> jobQueue,
  4 : optional bool live,
}

struct ServerInfo {
  1 : optional list<CommandInfo> reductionQueue,
  2 : optional list<WorkerInfo> workers
}

// TODO: Change to typedef.
struct Token {
    1 : optional string value
}

struct GetTokenResult {
  1 : optional Token token,
  2 : optional TokenError error,
}

// TODO: Change to typedef.
struct PlatformInfo {
    1 : optional string contents
}

struct ShaderFile {
    // E.g. "variant_1.frag"
    1 : optional string name,
    2 : optional string contents,
    3 : optional string info
}

enum ImageJobStatus {
  UNKNOWN = 0 // default value: should not be seen

  // Usually means "image was rendered", but could also mean
  // "compiled and linked shader successfully" if skipRender was true.
  SUCCESS = 10

  // The rendering process crashed.
  // `stage` will indicate when the crash occurred.
  CRASH = 20

  COMPILE_ERROR = 30
  LINK_ERROR = 40
  SANITY_ERROR = 45

  NONDET = 50 // image changed when rendering a few times

  // Timeout occurred.
  // `stage` will indicate when the timeout occurred.
  // `timeoutInfo` will give more details of each stage.
  TIMEOUT = 60

  // Something weird.
  // Often means reference shader failed to render
  // or vertex shader failed to compile.
  UNEXPECTED_ERROR = 70

  // Set by the server if the client fetched the job too many times
  // without giving a result.
  // I.e. the client keeps crashing and retrying this job.
  SKIPPED = 80

  SAME_AS_REFERENCE = 90
}

// Before starting a stage,
// a worker/client should record the stage that it is about to attempt.
enum ImageJobStage {
  NOT_STARTED = 0
  GET_JOB = 10
  START_JOB = 20
  PREPARE_REFERENCE = 30
  RENDER_REFERENCE = 40
  PREPARE_OTHER = 50
  VALIDATE_PROGRAM = 60
  RENDER_OTHER = 70
  REPLY_JOB = 80
}

struct ImageFile {
  // E.g. "variant_1.png"
  1 : optional string filename
  2 : optional binary contents
}

typedef i32 TimeInterval

struct TimingInfo {
  // In microseconds.
  1 : optional TimeInterval compilationTime
  2 : optional TimeInterval linkingTime
  3 : optional TimeInterval firstRenderTime
  4 : optional TimeInterval otherRendersTime
  5 : optional TimeInterval captureTime
}

struct ImageJobResult {
    // Deprecated fields:
    1 : optional ImageJobStatus status
    // FIXME: get rid of imageContents, use the imageFile below instead
    2 : optional binary imageContents
    3 : optional string errorMessage

    // Last stage that was attempted (but not completed)
    4 : optional ImageJobStage stage = ImageJobStage.NOT_STARTED
    // This could still be present, even if status2 is nondet
    // (i.e. it might be useful to see one of the images).
    5 : optional ImageFile imageFile
    6 : optional TimingInfo timingInfo
    // Provide a second image, e.g. in case of NONDET
    7 : optional ImageFile imageFile2
    8 : optional bool passSanityCheck
}


struct ImageJob {
    1 : optional ShaderFile reference,
    2 : optional ShaderFile shader,
    3 : optional ImageJobResult result,
    4 : optional string meta,
    5 : optional bool skipRender = false
}

// TODO: Change to typedef
struct JobId {
  1 : required i64 value
}

struct NoJob {
}

struct SkipJob {
}

struct Job {
    1 : required JobId jobId,
    2 : optional NoJob noJob,
    3 : optional ImageJob imageJob,
    4 : optional SkipJob skipJob,
}

struct CommandResult {
  1 : optional string output,
  2 : optional string error,
  3 : optional i32 exitCode,
}

exception TokenNotFoundException {
  1 : optional Token token
}


/**
* Our public FuzzerService interface.
**/
service FuzzerService {

  GetTokenResult getToken(1 : PlatformInfo platformInfo, 2 : Token token),

  Job getJob(1 : Token token) throws (1 : TokenNotFoundException ex),

  void jobDone(1 : Token token, 2 : Job job) throws (1 : TokenNotFoundException ex),
}

/**
* Each worker (previously client) (e.g. greylaptop, androidnexus, etc.)
* has two queues.
*
* A job queue typically only contains one or two jobs
* of type ImageJob. An ImageJob contains a shader and is
* retrieved by workers/clients who then render the shader
* and return the resulting PNG image.
*
* A command queue (previously work queue, and it is still WorkQueue in the code)
* contains potentially many commands (previously work items).
* Each command is literally a command;
* the command is run with the arguments given.
* E.g.
*   - run_shader_set shadersets/bbb --output processing/greylaptop/bbb_exp/
*
* The commands will typically queue jobs to a job queue for workers.
*
**/
service FuzzerServiceManager {

  /**
  * Submit a job (i.e. ImageJob) to a worker job queue.
  **/
  Job submitJob(1 : Job job, 2 : Token forClient, 3 : i32 retryLimit) throws (1 : TokenNotFoundException ex),

  /**
  * Clears a worker job queue.
  **/
  void clearClientJobQueue(1 : Token forClient),

  /**
  * Queues a command to a worker's command queue.
  **/
  void queueCommand(
    // E.g. "Run shaderset bbb"
    1 : string name,
    // E.g. run_shader_set shadersets/bbb --output processing/greylaptop/bbb_exp/
    2 : list<string> command,
    // This should generally be set to the token.
    3 : string queueName,
    // Optional: path to log file. E.g. processing/aaa/bbb_ccc_inv/command.log
    4 : string logFile),

  /**
  * Execute a command on the server immediately and return the result (including stdout).
  * No queue is used.
  **/
  CommandResult executeCommand(
    // E.g. "Querying results"
    1 : string name,
    //
    2 : list<string> command),

   ServerInfo getServerState(),

}
