package com.systemdesign.ecommarketplace.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * Mirrors src/common/decorators/current-user.decorator.ts's @CurrentUser().
 *
 * JwtAuthFilter sets the authenticated request's principal to the decoded
 * JwtPayload, so meta-annotating @AuthenticationPrincipal here is all that's
 * needed for Spring MVC to resolve `@CurrentUser JwtPayload user` controller
 * parameters the same way Nest resolves `@CurrentUser() user: JwtPayload`.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@AuthenticationPrincipal
public @interface CurrentUser {}
