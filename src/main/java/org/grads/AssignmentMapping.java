package org.grads;
/**
 * Represents a mapping between an assignment and a student
 */
public class AssignmentMapping {
    private final int assignmentIndex;
    private String studentName;
    private String studentId;
    private String studentEmail;
    
    public AssignmentMapping(int assignmentIndex, String studentName, String studentId, String studentEmail) {
        this.assignmentIndex = assignmentIndex;
        this.studentName = studentName;
        this.studentId = studentId;
        this.studentEmail = studentEmail;
    }
    
    public int getAssignmentIndex() {
        return assignmentIndex;
    }
    
    public String getStudentName() {
        return studentName;
    }
    
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public String getStudentEmail() {
        return studentEmail;
    }
    
    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }
    
    public void setStudent(String name, String id, String email) {
        this.studentName = name;
        this.studentId = id;
        this.studentEmail = email;
    }
    
    @Override
    public String toString() {
        return String.format("Assignment %d → %s (ID: %s)", 
            assignmentIndex + 1, studentName, studentId);
    }
}