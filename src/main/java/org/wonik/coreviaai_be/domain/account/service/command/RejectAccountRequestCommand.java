package org.wonik.coreviaai_be.domain.account.service.command;

public record RejectAccountRequestCommand(long version, String rejectReason) {
}
