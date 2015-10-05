/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.selfservice;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.http.HttpContext;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

/**
 * Unit test for {@link ForgottenPasswordRequestHandler}.
 *
 * @since 13.0.0
 */
public final class ForgottenPasswordRequestHandlerTest {

    private RequestHandler forgottenPassword;

    @Mock
    private ProgressStageFactory stageFactory;
    @Mock
    private ProgressStage<VerifyUserIdConfig> stage1;
    @Mock
    private SnapshotTokenHandlerFactory tokenHandlerFactory;
    @Mock
    private SnapshotTokenHandler tokenHandler;
    @Mock
    private ProcessStore processStore;

    private HttpContext context;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        context = new HttpContext(json(object(field("headers", Collections.emptyMap()),
                field("parameters", Collections.emptyMap()))), null);

        BaseURLProviderFactory baseURLProviderFactory = mock(BaseURLProviderFactory.class);
        BaseURLProvider urlProvider = mock(BaseURLProvider.class);
        given(baseURLProviderFactory.get("/")).willReturn(urlProvider);
        given(urlProvider.getURL(context)).willReturn("http://host:port/path");

        forgottenPassword = new ForgottenPasswordRequestHandler(
                stageFactory, tokenHandlerFactory, processStore, baseURLProviderFactory);
    }

    @Test
    public void initialReadReturnsBasicRequirements() throws ResourceException {
        // When
        ReadRequest request = Requests.newReadRequest("/forgottenPassword");

        given(stageFactory.get(isA(VerifyUserIdConfig.class))).willReturn(stage1);

        JsonValue initialRequirements = RequirementsBuilder
                .newInstance("test")
                .addRequireProperty("testParam", "Test param")
                .build();

        given(stage1.gatherInitialRequirements(isA(ProcessContext.class), isA(VerifyUserIdConfig.class)))
                .willReturn(initialRequirements);

        // Given
        Promise<ResourceResponse, ResourceException> promise = forgottenPassword.handleRead(context, request);

        // Then
        assertThat(promise).succeeded().withContent().stringAt("tag").isEqualTo("initial");
        assertThat(promise).succeeded().withContent()
                .hasObject("requirements").hasArray("required").containsExactly("testParam");
    }

    @Test
    public void initialActionReturnsCompletion() throws ResourceException {
        // When
        ActionRequest request = Requests.newActionRequest("/forgottenPassword", "submitRequirements");
        request.setContent(json(object(field("input", object()))));

        given(tokenHandlerFactory.get(isA(SnapshotTokenConfig.class))).willReturn(tokenHandler);
        given(stageFactory.get(isA(VerifyUserIdConfig.class))).willReturn(stage1);

        JsonValue additionalRequirements = RequirementsBuilder
                .newInstance("test")
                .addRequireProperty("testParam", "Test param")
                .build();

        StageResponse response = StageResponse
                .newBuilder()
                .setStageTag("nextStep")
                .setRequirements(additionalRequirements)
                .build();

        given(stage1.advance(isA(ProcessContext.class), isA(VerifyUserIdConfig.class)))
                .willReturn(response);
        given(tokenHandler.generate(isA(JsonValue.class))).willReturn("123456789");

        // Given
        Promise<ActionResponse, ResourceException> promise = forgottenPassword.handleAction(context, request);

        // Then
        assertThat(promise).succeeded().withContent().stringAt("tag").isEqualTo("nextStep");
        assertThat(promise).succeeded().withContent().stringAt("token").isEqualTo("123456789");
        assertThat(promise).succeeded().withContent()
                .hasObject("requirements").hasArray("required").containsExactly("testParam");
    }

}