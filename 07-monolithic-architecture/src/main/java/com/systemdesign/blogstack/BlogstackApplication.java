package com.systemdesign.blogstack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Single deployable, single process: the entire BlogStack application -- Auth, Users, Posts,
 * Comments, Notifications -- boots from this one entry point, mirroring {@code src/main.ts} in
 * the NestJS source. Unlike {@code 01-modular-monolith}, nothing here enforces boundaries between
 * modules: {@code CommentsService} injects and calls {@code PostsService}, {@code UsersService},
 * and {@code NotificationsService} directly, in-process, on the same request. That's the point of
 * this project -- it's the "everything calling everything" monolith the modular monolith exists
 * to fix.
 */
@SpringBootApplication
public class BlogstackApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogstackApplication.class, args);
    }
}
