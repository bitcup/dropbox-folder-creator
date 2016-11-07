package com.bitcup.dropboxfoldercreator.message;

import com.bitcup.dropboxfoldercreator.domain.Student;

/**
 * @author bitcup
 */
public abstract class CreationResult {
    public Student student;

    public CreationResult(Student student) {
        this.student = student;
    }

    public static final class Success extends CreationResult {
        public String topFolderLink;

        public Success(Student student, String topFolderLink) {
            super(student);
            this.topFolderLink = topFolderLink;
        }
    }

    public static final class Failure extends CreationResult {
        public Failure(Student student) {
            super(student);
        }
    }
}
