package com.wwj.service;

public interface GogsConfigFileService {
    String getConfigFromGogs(String fullName, String branchName, String filePath, String token);

}
