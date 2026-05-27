package com.example.gahramheit.dto;

import lombok.Data;
import java.util.List;

@Data
public class JikanCharacterResponse {
    private List<CharacterData> data;

    @Data
    public static class CharacterData {
        private List<VoiceActor> voice_actors;
    }

    @Data
    public static class VoiceActor {
        private String language;
        private Person person;
    }

    @Data
    public static class Person {
        private String name;
    }
}