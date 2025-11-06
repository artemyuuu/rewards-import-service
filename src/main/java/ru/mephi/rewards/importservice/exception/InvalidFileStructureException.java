package ru.mephi.rewards.importservice.exception;

public class InvalidFileStructureException extends RuntimeException {
    public InvalidFileStructureException(String message) {
        super(message);
    }
    public InvalidFileStructureException(String message, Throwable cause) {
        super(message, cause);
    }

}
