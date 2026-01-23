package dev.yewintnaing.protocol;

import java.util.List;

public record RespArray(List<RespType> elements) implements RespType {
}
