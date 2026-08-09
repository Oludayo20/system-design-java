package com.systemdesign.blogstack.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controller-method parameter annotation resolving to the authenticated
 * {@link com.systemdesign.blogstack.auth.AuthenticatedUser} for the current request. Mirrors
 * {@code src/shared/decorators/current-user.decorator.ts}'s {@code @CurrentUser()}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
