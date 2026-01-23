package dev.yewintnaing.protocol;

// All records must be public to be accessed by the Parser and Logic packages
public record RespString(String value) implements RespType {
}
