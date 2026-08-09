package com.systemdesign.faas.runtime;

/** Minimal test fixture with no external dependencies — echoes the event payload back. */
final class EchoFunction implements LambdaFunction {

    @Override
    public LambdaResponse handle(LambdaEvent event, LambdaContext context) {
        return new LambdaResponse(200, event.payload());
    }
}
