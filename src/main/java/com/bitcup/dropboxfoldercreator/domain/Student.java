package com.bitcup.dropboxfoldercreator.domain;

/**
 * @author bitcup
 */
public final class Student {
    private final String first;
    private final String last;
    private final String email;

    public Student(String first, String last, String email) {
        this.first = first;
        this.last = last;
        this.email = email;
    }

    public Student(String line) {
        String[] nameAndEmail = line.split(",");
        String[] firstAndLast = nameAndEmail[0].split(" ");
        this.first = firstAndLast[0];
        this.last = firstAndLast[1];
        this.email = nameAndEmail[1];
    }

    public String getFirst() {
        return first;
    }

    public String getLast() {
        return last;
    }

    public String getEmail() {
        return email;
    }

    public String getStudentFolderName() {
        return "/" + this.first + this.last;
    }

    @Override
    public String toString() {
        return new org.apache.commons.lang3.builder.ToStringBuilder(this)
                .append("first", first)
                .append("last", last)
                .append("email", email)
                .toString();
    }
}
