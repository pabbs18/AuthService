package io.ps.auth.messaging;

import org.springframework.cloud.stream.annotation.Output;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;


public interface UserEventChannel {
    String OUTPUT = "userChanged";

    @Output(OUTPUT)
    MessageChannel userChanged();
}
