package br.com.formentini.eventManager.event;

public class EventScheduleConflictException extends RuntimeException {

    public EventScheduleConflictException(String message) {
        super(message);
    }
}
