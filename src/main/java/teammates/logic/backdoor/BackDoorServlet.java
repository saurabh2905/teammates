package teammates.logic.backdoor;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackResponseAttributes;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.exception.TeammatesException;
import teammates.common.util.Config;
import teammates.common.util.Const;
import teammates.common.util.Utils;

@SuppressWarnings("serial")
public class BackDoorServlet extends HttpServlet {
    
    public enum BackDoorOperationCode {
        OPERATION_DELETE_ACCOUNT,
        OPERATION_DELETE_COURSE,
        OPERATION_DELETE_FEEDBACK_QUESTION,
        OPERATION_DELETE_FEEDBACK_RESPONSE,
        OPERATION_DELETE_FEEDBACK_SESSION,
        OPERATION_DELETE_INSTRUCTOR,
        OPERATION_DELETE_STUDENT,
        OPERATION_EDIT_FEEDBACK_QUESTION,
        OPERATION_EDIT_FEEDBACK_SESSION,
        OPERATION_EDIT_STUDENT,
        OPERATION_EDIT_STUDENT_PROFILE_PICTURE,
        OPERATION_GET_ACCOUNT_AS_JSON,
        OPERATION_GET_COURSE_AS_JSON,
        OPERATION_GET_ENCRYPTED_KEY_FOR_INSTRUCTOR,
        OPERATION_GET_ENCRYPTED_KEY_FOR_STUDENT,
        OPERATION_GET_FEEDBACK_QUESTION_AS_JSON,
        OPERATION_GET_FEEDBACK_QUESTION_FOR_ID_AS_JSON,
        OPERATION_GET_FEEDBACK_RESPONSE_AS_JSON,
        OPERATION_GET_FEEDBACK_RESPONSES_FOR_GIVER_AS_JSON,
        OPERATION_GET_FEEDBACK_RESPONSES_FOR_RECEIVER_AS_JSON,
        OPERATION_GET_FEEDBACK_SESSION_AS_JSON,
        OPERATION_GET_INSTRUCTOR_AS_JSON_BY_ID,
        OPERATION_GET_INSTRUCTOR_AS_JSON_BY_EMAIL,
        OPERATION_GET_STUDENT_AS_JSON,
        OPERATION_GET_STUDENTPROFILE_AS_JSON,
        OPERATION_IS_PICTURE_PRESENT_IN_GCS,
        OPERATION_PERSIST_DATABUNDLE,
        OPERATION_PUT_DOCUMENTS,
        OPERATION_REMOVE_AND_RESTORE_DATABUNDLE,
        OPERATION_REMOVE_DATABUNDLE
    }
    
    public static final String PARAMETER_BACKDOOR_KEY = "PARAMETER_BACKDOOR_KEY";
    public static final String PARAMETER_BACKDOOR_OPERATION = "PARAMETER_BACKDOOR_OPERATION";
    public static final String PARAMETER_GOOGLE_ID = "PARAMETER_GOOGLE_ID";
    public static final String PARAMETER_COURSE_ID = "PARAMETER_COURSE_ID";
    public static final String PARAMETER_FEEDBACK_QUESTION_ID = "PARAMETER_FEEDBACK_QUESTION_ID";
    public static final String PARAMETER_INSTRUCTOR_EMAIL = "PARAMETER_INSTRUCTOR_EMAIL";
    public static final String PARAMETER_INSTRUCTOR_ID = "PARAMETER_INSTRUCTOR_ID";
    public static final String PARAMETER_DATABUNDLE_JSON = "PARAMETER_DATABUNDLE_JSON";
    public static final String PARAMETER_JSON_STRING = "PARAMETER_JSON_STRING";
    public static final String PARAMETER_STUDENT_EMAIL = "PARAMETER_STUDENT_EMAIL";
    public static final String PARAMETER_FEEDBACK_SESSION_NAME = "PARAMETER_FEEDBACK_SESSION_NAME";
    public static final String PARAMETER_FEEDBACK_QUESTION_NUMBER = "PARAMETER_FEEDBACK_QUESTION_NUMBER";
    public static final String PARAMETER_GIVER_EMAIL = "PARAMETER_GIVER_EMAIL";
    public static final String PARAMETER_RECIPIENT = "PARAMETER_RECIPIENT";
    public static final String PARAMETER_PICTURE_KEY = "PARAMETER_PICTURE_KEY";
    public static final String PARAMETER_PICTURE_DATA = "PARAMETER_PICTURE_DATA";
    
    private static final Logger log = Utils.getLogger();

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String action = req.getParameter(PARAMETER_BACKDOOR_OPERATION);
        log.info(action);
        BackDoorOperationCode opCode = BackDoorOperationCode.valueOf(action);

        String returnValue;

        String keyReceived = req.getParameter(PARAMETER_BACKDOOR_KEY);
        
        resp.setContentType("text/plain; charset=utf-8");
        
        boolean isAuthorized = keyReceived.equals(Config.BACKDOOR_KEY);
        if (isAuthorized) {
            try {
                returnValue = executeBackEndAction(req, opCode);
            } catch (Exception e) {
                log.info(e.getMessage());
                returnValue = Const.StatusCodes.BACKDOOR_STATUS_FAILURE
                                                + TeammatesException.toStringWithStackTrace(e);
            } catch (AssertionError ae) {
                log.info(ae.getMessage());
                returnValue = Const.StatusCodes.BACKDOOR_STATUS_FAILURE
                                                + " Assertion error " + ae.getMessage();
            }
            resp.getWriter().write(returnValue);
        } else {
            resp.getWriter().write("Not authorized to access Backdoor Services");
        }
        resp.flushBuffer();
    }

    @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault") // no default so that each case is accounted for
    private String executeBackEndAction(HttpServletRequest req, BackDoorOperationCode opCode)
            throws IOException, InvalidParametersException, EntityDoesNotExistException {
        BackDoorLogic backDoorLogic = new BackDoorLogic();
        switch (opCode) {
        case OPERATION_DELETE_ACCOUNT:
            String googleId = req.getParameter(PARAMETER_GOOGLE_ID);
            backDoorLogic.deleteAccount(googleId);
            break;
        case OPERATION_DELETE_COURSE:
            String courseId = req.getParameter(PARAMETER_COURSE_ID);
            backDoorLogic.deleteCourse(courseId);
            break;
        case OPERATION_DELETE_FEEDBACK_QUESTION:
            String questionId = req.getParameter(PARAMETER_FEEDBACK_QUESTION_ID);
            backDoorLogic.deleteFeedbackQuestion(questionId);
            break;
        case OPERATION_DELETE_FEEDBACK_RESPONSE:
            String feedbackQuestionId = req.getParameter(PARAMETER_FEEDBACK_QUESTION_ID);
            String giverEmail = req.getParameter(PARAMETER_GIVER_EMAIL);
            String recipient = req.getParameter(PARAMETER_RECIPIENT);
            FeedbackResponseAttributes fr =
                    backDoorLogic.getFeedbackResponse(feedbackQuestionId, giverEmail, recipient);
            backDoorLogic.deleteFeedbackResponse(fr);
            break;
        case OPERATION_DELETE_FEEDBACK_SESSION:
            String feedbackSessionName = req.getParameter(PARAMETER_FEEDBACK_SESSION_NAME);
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            backDoorLogic.deleteFeedbackSession(feedbackSessionName, courseId);
            break;
        case OPERATION_DELETE_INSTRUCTOR:
            String instructorEmail = req.getParameter(PARAMETER_INSTRUCTOR_EMAIL);
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            backDoorLogic.deleteInstructor(courseId, instructorEmail);
            break;
        case OPERATION_DELETE_STUDENT:
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            String studentEmail = req.getParameter(PARAMETER_STUDENT_EMAIL);
            backDoorLogic.deleteStudent(courseId, studentEmail);
            break;
        case OPERATION_EDIT_FEEDBACK_QUESTION:
            String newValues = req.getParameter(PARAMETER_JSON_STRING);
            backDoorLogic.editFeedbackQuestionAsJson(newValues);
            break;
        case OPERATION_EDIT_FEEDBACK_SESSION:
            newValues = req.getParameter(PARAMETER_JSON_STRING);
            backDoorLogic.editFeedbackSessionAsJson(newValues);
            break;
        case OPERATION_EDIT_STUDENT:
            studentEmail = req.getParameter(PARAMETER_STUDENT_EMAIL);
            newValues = req.getParameter(PARAMETER_JSON_STRING);
            backDoorLogic.editStudentAsJson(studentEmail, newValues);
            break;
        case OPERATION_EDIT_STUDENT_PROFILE_PICTURE:
            String pictureDataString = req.getParameter(PARAMETER_PICTURE_DATA);
            byte[] pictureData = Utils.getTeammatesGson().fromJson(pictureDataString, byte[].class);
            googleId = req.getParameter(PARAMETER_GOOGLE_ID);
            backDoorLogic.uploadAndUpdateStudentProfilePicture(googleId, pictureData);
            break;
        case OPERATION_GET_ACCOUNT_AS_JSON:
            googleId = req.getParameter(PARAMETER_GOOGLE_ID);
            return backDoorLogic.getAccountAsJson(googleId);
        case OPERATION_GET_COURSE_AS_JSON:
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            return backDoorLogic.getCourseAsJson(courseId);
        case OPERATION_GET_ENCRYPTED_KEY_FOR_INSTRUCTOR:
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            studentEmail = req.getParameter(PARAMETER_INSTRUCTOR_EMAIL);
            return backDoorLogic.getEncryptedKeyForInstructor(courseId, studentEmail);
        case OPERATION_GET_ENCRYPTED_KEY_FOR_STUDENT:
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            studentEmail = req.getParameter(PARAMETER_STUDENT_EMAIL);
            return backDoorLogic.getEncryptedKeyForStudent(courseId, studentEmail);
        case OPERATION_GET_FEEDBACK_QUESTION_AS_JSON:
            feedbackSessionName = req.getParameter(PARAMETER_FEEDBACK_SESSION_NAME);
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            int qnNumber = Integer.parseInt(req.getParameter(PARAMETER_FEEDBACK_QUESTION_NUMBER));
            return backDoorLogic.getFeedbackQuestionAsJson(feedbackSessionName, courseId, qnNumber);
        case OPERATION_GET_FEEDBACK_QUESTION_FOR_ID_AS_JSON:
            questionId = req.getParameter(PARAMETER_FEEDBACK_QUESTION_ID);
            return backDoorLogic.getFeedbackQuestionForIdAsJson(questionId);
        case OPERATION_GET_FEEDBACK_RESPONSE_AS_JSON:
            feedbackQuestionId = req.getParameter(PARAMETER_FEEDBACK_QUESTION_ID);
            giverEmail = req.getParameter(PARAMETER_GIVER_EMAIL);
            recipient = req.getParameter(PARAMETER_RECIPIENT);
            return backDoorLogic.getFeedbackResponseAsJson(feedbackQuestionId, giverEmail, recipient);
        case OPERATION_GET_FEEDBACK_RESPONSES_FOR_GIVER_AS_JSON:
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            giverEmail = req.getParameter(PARAMETER_GIVER_EMAIL);
            return backDoorLogic.getFeedbackResponsesForGiverAsJson(courseId, giverEmail);
        case OPERATION_GET_FEEDBACK_RESPONSES_FOR_RECEIVER_AS_JSON:
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            recipient = req.getParameter(PARAMETER_RECIPIENT);
            return backDoorLogic.getFeedbackResponsesForReceiverAsJson(courseId, recipient);
        case OPERATION_GET_FEEDBACK_SESSION_AS_JSON:
            feedbackSessionName = req.getParameter(PARAMETER_FEEDBACK_SESSION_NAME);
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            return backDoorLogic.getFeedbackSessionAsJson(feedbackSessionName, courseId);
        case OPERATION_GET_INSTRUCTOR_AS_JSON_BY_ID:
            String instructorId = req.getParameter(PARAMETER_INSTRUCTOR_ID);
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            return backDoorLogic.getInstructorAsJsonById(instructorId, courseId);
        case OPERATION_GET_INSTRUCTOR_AS_JSON_BY_EMAIL:
            instructorEmail = req.getParameter(PARAMETER_INSTRUCTOR_EMAIL);
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            return backDoorLogic.getInstructorAsJsonByEmail(instructorEmail, courseId);
        case OPERATION_GET_STUDENT_AS_JSON:
            courseId = req.getParameter(PARAMETER_COURSE_ID);
            studentEmail = req.getParameter(PARAMETER_STUDENT_EMAIL);
            return backDoorLogic.getStudentAsJson(courseId, studentEmail);
        case OPERATION_GET_STUDENTPROFILE_AS_JSON:
            googleId = req.getParameter(PARAMETER_GOOGLE_ID);
            return backDoorLogic.getStudentProfileAsJson(googleId);
        case OPERATION_IS_PICTURE_PRESENT_IN_GCS:
            String pictureKey = req.getParameter(PARAMETER_PICTURE_KEY);
            return String.valueOf(backDoorLogic.isPicturePresentInGcs(pictureKey));
        case OPERATION_PERSIST_DATABUNDLE:
            String dataBundleJsonString = req.getParameter(PARAMETER_DATABUNDLE_JSON);
            DataBundle dataBundle = Utils.getTeammatesGson().fromJson(dataBundleJsonString, DataBundle.class);
            backDoorLogic.persistDataBundle(dataBundle);
            break;
        case OPERATION_PUT_DOCUMENTS:
            dataBundleJsonString = req.getParameter(PARAMETER_DATABUNDLE_JSON);
            dataBundle = Utils.getTeammatesGson().fromJson(dataBundleJsonString, DataBundle.class);
            backDoorLogic.putDocuments(dataBundle);
            break;
        case OPERATION_REMOVE_AND_RESTORE_DATABUNDLE:
            dataBundleJsonString = req.getParameter(PARAMETER_DATABUNDLE_JSON);
            dataBundle = Utils.getTeammatesGson().fromJson(dataBundleJsonString, DataBundle.class);
            backDoorLogic.removeDataBundle(dataBundle);
            backDoorLogic.persistDataBundle(dataBundle);
            break;
        case OPERATION_REMOVE_DATABUNDLE:
            dataBundleJsonString = req.getParameter(PARAMETER_DATABUNDLE_JSON);
            dataBundle = Utils.getTeammatesGson().fromJson(dataBundleJsonString, DataBundle.class);
            backDoorLogic.removeDataBundle(dataBundle);
            break;
        }
        return Const.StatusCodes.BACKDOOR_STATUS_SUCCESS;
    }

}
