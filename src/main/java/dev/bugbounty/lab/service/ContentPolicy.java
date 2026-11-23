package dev.bugbounty.lab.service;

public interface ContentPolicy {
    String onWrite(String input);
}
