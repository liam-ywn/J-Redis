package dev.yewintnaing.protocol;

public sealed interface RespType
                permits RespString, RespInt, RespNull, RespArray, RespError {
}
