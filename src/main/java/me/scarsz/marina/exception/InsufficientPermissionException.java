package me.scarsz.marina.exception;

import lombok.Getter;

public class InsufficientPermissionException extends Throwable {

    @Getter private final String permission;

    public InsufficientPermissionException(String permission) {
        super("Missing permission: " + permission);
        this.permission = permission;
    }

}
