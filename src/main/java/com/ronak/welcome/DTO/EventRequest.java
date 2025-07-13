package com.ronak.welcome.DTO;


import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record EventRequest(
        @NotBlank String name,
        String description,
        @NotNull @FutureOrPresent LocalDateTime eventDate,
        @NotBlank String location
) {}