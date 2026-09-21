package com.cloudpilot.backend.rbac;

public enum PermissionName {

    PROJECT_CREATE,
    PROJECT_READ,
    PROJECT_UPDATE,
    PROJECT_DELETE,

    TASK_CREATE,
    TASK_READ,
    TASK_UPDATE,
    TASK_DELETE,

    USER_READ,
    USER_INVITE,
    USER_REMOVE,

    ROLE_READ,
    ROLE_ASSIGN,

    BILLING_READ,
    BILLING_UPDATE,

    SETTINGS_READ,
    SETTINGS_UPDATE
}