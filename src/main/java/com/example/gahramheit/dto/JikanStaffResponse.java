package com.example.gahramheit.dto;

import lombok.Data;
import java.util.List;

@Data
public class JikanStaffResponse {
    private List<StaffData> data;

    @Data
    public static class StaffData {
        private List<String> positions;
        private Person person;
    }

    @Data
    public static class Person {
        private String name;
    }
}