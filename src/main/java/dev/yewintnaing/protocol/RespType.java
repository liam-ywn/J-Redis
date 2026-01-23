package dev.yewintnaing.protocol;

import java.util.List;

public sealed interface RespType
        permits RespString, RespInt, RespNull, RespArray, RespError {
}

