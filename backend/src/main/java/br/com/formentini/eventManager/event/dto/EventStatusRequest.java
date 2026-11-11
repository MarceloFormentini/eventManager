package br.com.formentini.eventManager.event.dto;

import br.com.formentini.eventManager.event.enums.EventStatus;
import jakarta.validation.constraints.NotNull;

public record EventStatusRequest (
    @NotNull 
    EventStatus status
){
}
