package teammates.test.cases.testdriver;

import java.util.Map;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.AccountAttributes;
import teammates.common.datatransfer.CourseAttributes;
import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.common.datatransfer.FeedbackResponseAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.datatransfer.StudentAttributes;
import teammates.common.datatransfer.StudentProfileAttributes;
import teammates.common.util.Const;
import teammates.common.util.StringHelper;
import teammates.common.util.Utils;
import teammates.test.cases.BaseTestCase;
import teammates.test.driver.BackDoor;
import teammates.test.util.Priority;

import com.google.appengine.api.datastore.Text;
import com.google.gson.Gson;

@Priority(2)
public class BackDoorTest extends BaseTestCase {

    private static Gson gson = Utils.getTeammatesGson();
    private static DataBundle dataBundle = getTypicalDataBundle();

    @BeforeClass
    public void classSetup() {
        printTestClassHeader();
        String status = Const.StatusCodes.BACKDOOR_STATUS_FAILURE;
        int retryLimit = 5;
        while (status.startsWith(Const.StatusCodes.BACKDOOR_STATUS_FAILURE) && retryLimit > 0) {
            status = BackDoor.removeAndRestoreDataBundleFromDb(dataBundle);
            retryLimit--;
        }
        assertEquals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS, status);
        
        // verifies that typical bundle is restored by the above operation
        verifyPresentInDatastore(dataBundle);
    }

    @Test
    public void testDeletion() {
        
        // ----------deleting Instructor entities-------------------------
        InstructorAttributes instructor1OfCourse1 = dataBundle.instructors.get("instructor2OfCourse2");
        verifyPresentInDatastore(instructor1OfCourse1);
        String status = BackDoor.deleteInstructor(instructor1OfCourse1.courseId, instructor1OfCourse1.email);
        assertEquals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS, status);
        verifyAbsentInDatastore(instructor1OfCourse1);
        
        //try to delete again: should indicate as success because delete fails silently.
        status = BackDoor.deleteInstructor(instructor1OfCourse1.email, instructor1OfCourse1.courseId);
        assertEquals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS, status);

        // ----------deleting Feedback Response entities-------------------------
        FeedbackQuestionAttributes fq = dataBundle.feedbackQuestions.get("qn2InSession1InCourse1");
        FeedbackResponseAttributes fr = dataBundle.feedbackResponses.get("response1ForQ2S1C1");
        fq = BackDoor.getFeedbackQuestion(fq.courseId, fq.feedbackSessionName, fq.questionNumber);
        fr = BackDoor.getFeedbackResponse(fq.getId(), fr.giver, fr.recipient);
        
        verifyPresentInDatastore(fr);
        status = BackDoor.deleteFeedbackResponse(fq.getId(), fr.giver, fr.recipient);
        assertEquals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS, status);
        verifyAbsentInDatastore(fr);
        
        // ----------deleting Feedback Question entities-------------------------
        fq = dataBundle.feedbackQuestions.get("qn1InSession1InCourse1");
        verifyPresentInDatastore(fq);
        status = BackDoor.deleteFeedbackQuestion(fq.getId());
        assertEquals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS, status);
        verifyAbsentInDatastore(fq);

        // ----------deleting Course entities-------------------------
        // #COURSE 2
        CourseAttributes course2 = dataBundle.courses.get("typicalCourse2");
        verifyPresentInDatastore(course2);
        status = BackDoor.deleteCourse(course2.getId());
        assertEquals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS, status);
        verifyAbsentInDatastore(course2);

        // check if related student entities are also deleted
        StudentAttributes student2InCourse2 = dataBundle.students
                .get("student2InCourse2");
        verifyAbsentInDatastore(student2InCourse2);
        
        // #COURSE 1
        CourseAttributes course1 = dataBundle.courses.get("typicalCourse1");
        verifyPresentInDatastore(course1);
        status = BackDoor.deleteCourse(course1.getId());
        assertEquals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS, status);
        verifyAbsentInDatastore(course1);
        
        // check if related student entities are also deleted
        StudentAttributes student1InCourse1 = dataBundle.students
                .get("student1InCourse1");
        verifyAbsentInDatastore(student1InCourse1);

        // #COURSE NO EVALS
        CourseAttributes courseNoEvals = dataBundle.courses.get("courseNoEvals");
        verifyPresentInDatastore(courseNoEvals);
        status = BackDoor.deleteCourse(courseNoEvals.getId());
        assertEquals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS, status);
        verifyAbsentInDatastore(courseNoEvals);
        
        // ----------deleting Feedback Session entities-------------------------
        // TODO: do proper deletion test

    }
    
    @Test
    public void testCreateAccount() {
        AccountAttributes newAccount = dataBundle.accounts.get("instructor1OfCourse1");
        
        // Make sure not already inside
        BackDoor.deleteAccount(newAccount.googleId);
        verifyAbsentInDatastore(newAccount);
        
        // Perform creation
        BackDoor.createAccount(newAccount);
        verifyPresentInDatastore(newAccount);
        
        // Clean up
        BackDoor.deleteAccount(newAccount.googleId);
        verifyAbsentInDatastore(newAccount);
    }
    
    public void testGetAccount() {
        // already tested by testPersistenceAndDeletion
    }
    
    public void testDeleteAccount() {
        // already tested by testPersistenceAndDeletion
    }

    public void testDeleteInstructors() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testCreateInstructor() {
        // only minimal testing because this is a wrapper method for
        // another well-tested method.

        String instructorId = "tmapitt.tcc.instructor";
        String courseId = "tmapitt.tcc.course";
        String name = "Tmapitt testInstr Name";
        String email = "tmapitt@tci.tmt";
        @SuppressWarnings("deprecation")
        InstructorAttributes instructor = new InstructorAttributes(instructorId, courseId, name, email);
        
        // Make sure not already inside
        BackDoor.deleteInstructor(courseId, email);
        verifyAbsentInDatastore(instructor);
        
        // Perform creation
        BackDoor.createInstructor(instructor);
        verifyPresentInDatastore(instructor);
        instructor = BackDoor.getInstructorByEmail(email, courseId);
        // Clean up
        BackDoor.deleteInstructor(courseId, email);
        BackDoor.deleteAccount(instructor.googleId);
        verifyAbsentInDatastore(instructor);
    }

    public void testGetInstructorAsJson() {
        // already tested by testPersistenceAndDeletion
    }

    public void testDeleteInstructor() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testCreateCourse() {
        // only minimal testing because this is a wrapper method for
        // another well-tested method.

        String courseId = "tmapitt.tcc.course";
        CourseAttributes course = new CourseAttributes(courseId,
                "Name of tmapitt.tcc.instructor", "UTC");
        
        // Make sure not already inside
        BackDoor.deleteCourse(courseId);
        verifyAbsentInDatastore(course);
        
        // Perform creation
        BackDoor.createCourse(course);
        verifyPresentInDatastore(course);
        
        // Clean up
        BackDoor.deleteCourse(courseId);
        verifyAbsentInDatastore(course);
    }

    public void testGetCourseAsJson() {
        // already tested by testPersistenceAndDeletion
    }
    
    public void testDeleteCourse() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testCreateStudent() {
        // only minimal testing because this is a wrapper method for
        // another well-tested method.

        StudentAttributes student = new StudentAttributes(
                "section name", "team name", "name of tcs student", "tcsStudent@gmail.tmt", "",
                "tmapit.tcs.course");
        BackDoor.deleteStudent(student.course, student.email);
        verifyAbsentInDatastore(student);
        BackDoor.createStudent(student);
        verifyPresentInDatastore(student);
        BackDoor.deleteStudent(student.course, student.email);
        verifyAbsentInDatastore(student);
    }

    @Test
    public void testGetEncryptedKeyForStudent() {

        StudentAttributes student = new StudentAttributes("sect1", "t1", "name of tgsr student",
                                                          "tgsr@gmail.tmt", "", "course1");
        BackDoor.createStudent(student);
        String key = Const.StatusCodes.BACKDOOR_STATUS_FAILURE;
        int retryLimit = 5;
        while (key.startsWith(Const.StatusCodes.BACKDOOR_STATUS_FAILURE) && retryLimit > 0) {
            key = BackDoor.getEncryptedKeyForStudent(student.course, student.email);
            retryLimit--;
        }

        // The following is the google app engine description about generating
        // keys.
        //
        // A key can be converted to a string by passing the Key object to
        // str(). The string is "urlsafe"—it uses only characters valid for use in URLs.
        //
        // RFC3986 definition of a safe url pattern
        // Characters that are allowed in a URI but do not have a reserved
        // purpose are called unreserved.
        // unreserved = ALPHA / DIGIT / "-" / "." / "_" / "~"
        String pattern = "(\\w|-|~|\\.)*";

        String errorMessage = key + "[length=" + key.length() + "][reg="
                + StringHelper.isMatching(key, pattern) + "] is not as expected";
        assertTrue(errorMessage, key.length() > 30 && StringHelper.isMatching(key, pattern));

        // clean up student as this is an orphan entity
        BackDoor.deleteStudent(student.course, student.email);

    }

    public void testGetStudentAsJson() {
        // already tested by testPersistenceAndDeletion
    }

    @Test
    public void testEditStudent() {

        // check for successful edit
        StudentAttributes student = dataBundle.students.get("student4InCourse1");
        // try to create the entity in case it does not exist
        BackDoor.createStudent(student);
        verifyPresentInDatastore(student);
        
        String originalEmail = student.email;
        student.name = "New name";
        student.lastName = "name";
        student.email = "new@gmail.tmt";
        student.comments = "new comments";
        student.team = "new team";
        String status = Const.StatusCodes.BACKDOOR_STATUS_FAILURE;
        int retryLimit = 5;
        while (status.startsWith(Const.StatusCodes.BACKDOOR_STATUS_FAILURE) && retryLimit > 0) {
            status = BackDoor.editStudent(originalEmail, student);
            retryLimit--;
        }
        assertEquals(Const.StatusCodes.BACKDOOR_STATUS_SUCCESS, status);
        verifyPresentInDatastore(student);

        // test for unsuccessful edit
        student.course = "non-existent";
        status = BackDoor.editStudent(originalEmail, student);
        assertTrue(status.startsWith(Const.StatusCodes.BACKDOOR_STATUS_FAILURE));
        verifyAbsentInDatastore(student);
    }

    public void testDeleteStudent() {
        // already tested by testPersistenceAndDeletion
    }
    
    @Test
    public void testCreateFeedbackResponse() {

        FeedbackResponseAttributes fr = new FeedbackResponseAttributes();
        FeedbackQuestionAttributes fq = dataBundle.feedbackQuestions.get("qn1InSession1InCourse1");
        StudentAttributes student = dataBundle.students.get("student3InCourse1");

        fq = BackDoor.getFeedbackQuestion(fq.courseId, fq.feedbackSessionName, fq.questionNumber);

        fr.feedbackSessionName = fq.feedbackSessionName;
        fr.courseId = fq.courseId;
        fr.feedbackQuestionId = fq.getId();
        fr.feedbackQuestionType = fq.questionType;
        fr.giver = student.email;
        fr.giverSection = student.section;
        fr.recipient = student.email;
        fr.recipientSection = student.section;
        fr.responseMetaData = new Text("Student 3 self feedback");
        fr.setId(fq.getId() + "%" + fr.giver + "%" + fr.recipient);

        // Make sure not already inside
        BackDoor.deleteFeedbackResponse(fr.feedbackQuestionId, fr.giver, fr.recipient);
        verifyAbsentInDatastore(fr);

        // Perform creation
        BackDoor.createFeedbackResponse(fr);
        verifyPresentInDatastore(fr);

        // Clean up
        BackDoor.deleteFeedbackResponse(fr.feedbackQuestionId, fr.giver, fr.recipient);
        verifyAbsentInDatastore(fr);
    }
    
    private void verifyAbsentInDatastore(AccountAttributes account) {
        assertNull(BackDoor.getAccount(account.googleId));
    }
    
    private void verifyAbsentInDatastore(CourseAttributes course) {
        assertNull(BackDoor.getCourse(course.getId()));
    }
    
    private void verifyAbsentInDatastore(InstructorAttributes expectedInstructor) {
        assertNull(BackDoor.getInstructorByEmail(expectedInstructor.email, expectedInstructor.courseId));
    }

    private void verifyAbsentInDatastore(StudentAttributes student) {
        assertNull(BackDoor.getStudent(student.course, student.email));
    }

    private void verifyAbsentInDatastore(FeedbackQuestionAttributes fq) {
        assertNull(BackDoor.getFeedbackQuestion(fq.getId()));
    }
    
    private void verifyAbsentInDatastore(FeedbackResponseAttributes fr) {
        assertNull(BackDoor.getFeedbackResponse(fr.feedbackQuestionId, fr.giver, fr.recipient));
    }

    private void verifyPresentInDatastore(DataBundle data) {

        Map<String, AccountAttributes> accounts = data.accounts;
        for (AccountAttributes expectedAccount : accounts.values()) {
            verifyPresentInDatastore(expectedAccount);
        }

        Map<String, CourseAttributes> courses = data.courses;
        for (CourseAttributes expectedCourse : courses.values()) {
            verifyPresentInDatastore(expectedCourse);
        }
        
        Map<String, InstructorAttributes> instructors = data.instructors;
        for (InstructorAttributes expectedInstructor : instructors.values()) {
            verifyPresentInDatastore(expectedInstructor);
        }

        Map<String, StudentAttributes> students = data.students;
        for (StudentAttributes expectedStudent : students.values()) {
            verifyPresentInDatastore(expectedStudent);
        }

    }

    private void verifyPresentInDatastore(StudentAttributes expectedStudent) {
        StudentAttributes actualStudent = null;
        int retryLimit = 5;
        while (actualStudent == null && retryLimit > 0) {
            actualStudent = BackDoor.getStudent(expectedStudent.course, expectedStudent.email);
            retryLimit--;
        }
        equalizeIrrelevantData(expectedStudent, actualStudent);
        expectedStudent.lastName = StringHelper.splitName(expectedStudent.name)[1];
        assertEquals(gson.toJson(expectedStudent), gson.toJson(actualStudent));
    }

    private void verifyPresentInDatastore(CourseAttributes expectedCourse) {
        CourseAttributes actualCourse = null;
        int retryLimit = 5;
        while (actualCourse == null && retryLimit > 0) {
            actualCourse = BackDoor.getCourse(expectedCourse.getId());
            retryLimit--;
        }
        // Ignore time field as it is stamped at the time of creation in testing
        actualCourse.createdAt = expectedCourse.createdAt;
        assertEquals(gson.toJson(expectedCourse), gson.toJson(actualCourse));
    }

    private void verifyPresentInDatastore(InstructorAttributes expectedInstructor) {
        InstructorAttributes actualInstructor = null;
        int retryLimit = 5;
        while (actualInstructor == null && retryLimit > 0) {
            actualInstructor = BackDoor.getInstructorByEmail(expectedInstructor.email, expectedInstructor.courseId);
            retryLimit--;
        }
        
        equalizeIrrelevantData(expectedInstructor, actualInstructor);
        assertTrue(expectedInstructor.isEqualToAnotherInstructor(actualInstructor));
    }
    
    private void verifyPresentInDatastore(AccountAttributes expectedAccount) {
        AccountAttributes actualAccount = BackDoor.getAccount(expectedAccount.googleId);
        // Ignore time field as it is stamped at the time of creation in testing
        actualAccount.createdAt = expectedAccount.createdAt;
        
        if (expectedAccount.studentProfile == null) {
            expectedAccount.studentProfile = new StudentProfileAttributes();
            expectedAccount.studentProfile.googleId = expectedAccount.googleId;
        }
        expectedAccount.studentProfile.modifiedDate = actualAccount.studentProfile.modifiedDate;
        assertEquals(gson.toJson(expectedAccount), gson.toJson(actualAccount));
    }

    private void verifyPresentInDatastore(FeedbackQuestionAttributes expectedQuestion) {
        FeedbackQuestionAttributes actualQuestion =
                BackDoor.getFeedbackQuestion(expectedQuestion.courseId, expectedQuestion.feedbackSessionName,
                                             expectedQuestion.questionNumber);
        
        // Match the id of the expected Feedback Question because it is not known in advance
        equalizeId(expectedQuestion, actualQuestion);
        assertEquals(gson.toJson(expectedQuestion), gson.toJson(actualQuestion));
    }

    private void verifyPresentInDatastore(FeedbackResponseAttributes expectedResponse) {
        FeedbackResponseAttributes actualResponse =
                BackDoor.getFeedbackResponse(expectedResponse.feedbackQuestionId, expectedResponse.giver,
                                             expectedResponse.recipient);

        assertEquals(gson.toJson(expectedResponse), gson.toJson(actualResponse));
    }

    private void equalizeIrrelevantData(
            StudentAttributes expectedStudent,
            StudentAttributes actualStudent) {
        
        // For these fields, we consider null and "" equivalent.
        if (expectedStudent.googleId == null && actualStudent.googleId.isEmpty()) {
            actualStudent.googleId = null;
        }
        if (expectedStudent.team == null && actualStudent.team.isEmpty()) {
            actualStudent.team = null;
        }
        if (expectedStudent.comments == null
                && actualStudent.comments.isEmpty()) {
            actualStudent.comments = null;
        }

        // pretend keys match because the key is generated on the server side
        // and cannot be anticipated
        if (actualStudent.key != null) {
            expectedStudent.key = actualStudent.key;
        }
    }
    
    private void equalizeIrrelevantData(
            InstructorAttributes expectedInstructor,
            InstructorAttributes actualInstructor) {
        
        // pretend keys match because the key is generated only before storing into database
        if (actualInstructor.key != null) {
            expectedInstructor.key = actualInstructor.key;
        }
    }

    private void equalizeId(
            FeedbackQuestionAttributes expectedFeedbackQuestion,
            FeedbackQuestionAttributes actualFeedbackQuestion) {

        expectedFeedbackQuestion.setId(actualFeedbackQuestion.getId());
    }

    @AfterClass
    public static void tearDown() {
        printTestClassFooter();
    }
}
