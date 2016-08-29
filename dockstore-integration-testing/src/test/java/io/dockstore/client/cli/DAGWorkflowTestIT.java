/*
 *    Copyright 2016 OICR
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.dockstore.client.cli;

import com.google.common.io.Resources;
import io.dockstore.webservice.DockstoreWebserviceApplication;
import io.dockstore.webservice.DockstoreWebserviceConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.UsersApi;
import io.swagger.client.api.WorkflowsApi;
import io.swagger.client.model.PublishRequest;
import io.swagger.client.model.Workflow;
import io.swagger.client.model.WorkflowVersion;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.SystemOutRule;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static io.dockstore.common.CommonTestUtilities.clearStateMakePrivate2;

/**
 * Created by jpatricia on 24/06/16.
 */
public class DAGWorkflowTestIT {

    @ClassRule
    public static final DropwizardAppRule<DockstoreWebserviceConfiguration> RULE = new DropwizardAppRule<>(
            DockstoreWebserviceApplication.class, ResourceHelpers.resourceFilePath("dockstoreTest.yml"));

    @Before
    public void clearDBandSetup() throws IOException, TimeoutException, ApiException {
        clearStateMakePrivate2();
    }

    @Rule
    public final ExpectedSystemExit systemExit = ExpectedSystemExit.none();

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    private WorkflowsApi setupWebService() throws IOException, TimeoutException, ApiException{
        ApiClient webClient = WorkflowET.getWebClient();
        WorkflowsApi workflowApi = new WorkflowsApi(webClient);

        UsersApi usersApi = new UsersApi(webClient);
        final Long userId = usersApi.getUser().getId();

        // Make publish request (true)
        final PublishRequest publishRequest = new PublishRequest();
        publishRequest.setPublish(true);

        // Get workflows
        usersApi.refreshWorkflows(userId);

        return workflowApi;
    }

    private List<String> getJSON(String repo, String fileName, String descType, String branch) throws IOException, TimeoutException, ApiException{
        WorkflowsApi workflowApi = setupWebService();
        Workflow githubWorkflow = workflowApi.manualRegister("github", repo, fileName, "test-workflow", descType);

        // Publish github workflow
        Workflow refresh = workflowApi.refresh(githubWorkflow.getId());
        Optional<WorkflowVersion> master = refresh.getWorkflowVersions().stream().filter(workflow -> workflow.getName().equals(branch)).findFirst();

        //getting the dag json string
        final String basePath = WorkflowET.getWebClient().getBasePath();
        URL url = new URL(basePath + "/workflows/" +githubWorkflow.getId()+"/dag/" + master.get().getId() );
        List<String> strings = Resources.readLines(url, Charset.forName("UTF-8"));

        return strings;
    }

    private int countNodeInJSON(List<String> strings){
        //count the number of nodes in the DAG json
        int countNode = 0;
        int last = 0;
        String node = "id";
        while(last !=-1){
            last = strings.get(0).indexOf(node,last);

            if(last !=-1){
                countNode++;
                last += node.length();
            }
        }

        return countNode;
    }


    @Test
    public void testWorkflowDAGCWL() throws IOException, TimeoutException, ApiException {
        // Input: 1st-workflow.cwl
        // Repo: test_workflow_cwl
        // Branch: master
        // Test: normal cwl workflow DAG
        // Return: JSON with 2 nodes and an edge connecting it (nodes:{{untar},{compile}}, edges:{untar->compile})

        final List<String> strings = getJSON("DockstoreTestUser2/test_workflow_cwl", "/1st-workflow.cwl", "cwl", "master");
        int countNode = countNodeInJSON(strings);

        Assert.assertTrue("JSON should not be blank", strings.size() > 0);
        Assert.assertEquals("JSON should have two nodes", countNode, 2);
        Assert.assertTrue("node data should have untar as tool", strings.get(0).contains("untar"));
        Assert.assertTrue("node data should have compile as tool", strings.get(0).contains("compile"));
        Assert.assertTrue("edge should connect untar and compile", strings.get(0).contains("\"source\":\"0\",\"target\":\"1\""));

    }

    @Test
    public void testWorkflowDAGWDLSingleNode() throws IOException, TimeoutException, ApiException {
        // Input: hello.wdl
        // Repo: test_workflow_wdl
        // Branch: master
        // Test: normal wdl workflow DAG, single node
        // Return: JSON with a node and no edge (nodes:{{hello}},edges:{}

        final List<String> strings = getJSON("DockstoreTestUser2/test_workflow_wdl", "/hello.wdl", "wdl", "master");
        int countNode = countNodeInJSON(strings);

        Assert.assertTrue("JSON should not be blank", strings.size() > 0);
        Assert.assertEquals("JSON should have one node", countNode,1);
        Assert.assertTrue("node data should have hello as task", strings.get(0).contains("hello"));
        Assert.assertTrue("should have no edge", strings.get(0).contains("\"edges\":[]"));
    }

    @Test
    public void testWorkflowDAGWDLMultipleNodes() throws IOException, TimeoutException, ApiException {
        // Input: hello.wdl
        // Repo: hello-dockstore-workflow
        // Branch: master
        // Test: normal wdl workflow DAG, multiple nodes
        // Return: JSON with a node and no edge (nodes:{{ps},{cgrep},{wc}},edges:{ps->cgrep, cgrep->wc}

        final List<String> strings = getJSON("DockstoreTestUser2/hello-dockstore-workflow", "/Dockstore.wdl", "wdl", "testWDL");
        int countNode = countNodeInJSON(strings);

        Assert.assertTrue("JSON should not be blank", strings.size() > 0);
        Assert.assertEquals("JSON should have three nodes", countNode,3);
        Assert.assertTrue("node data should have ps as tool", strings.get(0).contains("ps"));
        Assert.assertTrue("node data should have cgrep as tool", strings.get(0).contains("cgrep"));
        Assert.assertTrue("node data should have wc as tool", strings.get(0).contains("wc"));
        Assert.assertTrue("should have no edge", strings.get(0).contains("\"source\":\"0\",\"target\":\"1\""));
        Assert.assertTrue("should have no edge", strings.get(0).contains("\"source\":\"1\",\"target\":\"2\""));
    }

    @Test
    public void testWorkflowDAGCWLMissingTool() throws IOException, TimeoutException, ApiException {
        // Input: Dockstore.cwl
        // Repo: hello-dockstore-workflow
        // Branch: testCWL
        // Test: Repo does not have required tool files called by workflow file
        // Return: JSON not blank, but it will have empty nodes and edges (nodes:{},edges:{})

        final List<String> strings = getJSON("DockstoreTestUser2/hello-dockstore-workflow", "/Dockstore.cwl", "cwl", "testCWL");

        //JSON will have node:[] and edges:[]
        Assert.assertEquals("JSON should not have any data for nodes and edges", strings.size(),1);
    }

    @Test
    public void testWorkflowDAGWDLMissingTask() throws IOException, TimeoutException, ApiException {
        // Input: hello.wdl
        // Repo: test_workflow_wdl
        // Branch: missing_docker
        // Test: task called by workflow not found in the file
        // Return: blank JSON

        final List<String> strings = getJSON("DockstoreTestUser2/test_workflow_wdl", "/hello.wdl", "wdl", "missing_docker");

        //JSON will have no content at all
        Assert.assertEquals("JSON should be blank", strings.size(),0);
    }

    @Test
    public void testDAGImportSyntax() throws IOException, TimeoutException, ApiException {
        // Input: Dockstore.cwl
        // Repo: dockstore-whalesay-imports
        // Branch: master
        // Test: "run: {import:.....}"
        // Return: DAG with two nodes and an edge connecting it (nodes:{{rev},{sorted}}, edges:{rev->sorted})

        final List<String> strings = getJSON("DockstoreTestUser2/dockstore-whalesay-imports", "/Dockstore.cwl", "cwl", "master");
        int countNode = countNodeInJSON(strings);

        Assert.assertTrue("JSON should not be blank", strings.size() > 0);
        Assert.assertEquals("JSON should have two nodes", countNode, 2);
        Assert.assertTrue("node data should have rev as tool", strings.get(0).contains("rev"));
        Assert.assertTrue("node data should have sorted as tool", strings.get(0).contains("sorted"));
        Assert.assertTrue("edge should connect rev and sorted", strings.get(0).contains("\"source\":\"0\",\"target\":\"1\""));
    }

    @Test
    public void testDAGCWL1Syntax() throws IOException, TimeoutException, ApiException {
        // Input: preprocess_vcf.cwl
        // Repo: OxoG-Dockstore-Tools
        // Branch: develop
        // Test: "[pass_filter -> [inputs: ...., outputs: ....]] instead of [id->pass_filter,inputs->....]"
        // Return: DAG with 17 nodes

        final List<String> strings = getJSON("DockstoreTestUser2/OxoG-Dockstore-Tools", "/preprocess_vcf.cwl", "cwl", "develop");
        int countNode = countNodeInJSON(strings);

        Assert.assertTrue("JSON should not be blank", strings.size() > 0);
        Assert.assertEquals("JSON should have 17 nodes", countNode, 17);
        Assert.assertTrue("node data should have pass_filter as tool", strings.get(0).contains("pass_filter"));
        Assert.assertTrue("node data should have merge_vcfs as tool", strings.get(0).contains("merge_vcfs"));
    }

    @Test
    public void testHintsExpressionTool() throws IOException, TimeoutException, ApiException {
        // Input: preprocess_vcf.cwl
        // Repo: OxoG-Dockstore-Tools
        // Branch: hints_ExpressionTool
        // Test: "filter has a docker requirement inside expression Tool, linked to ubuntu"
        // Return: DAG with 17 nodes

        final List<String> strings = getJSON("DockstoreTestUser2/OxoG-Dockstore-Tools", "/preprocess_vcf.cwl", "cwl", "hints_ExpressionTool");
        int countNode = countNodeInJSON(strings);

        Assert.assertTrue("JSON should not be blank", strings.size() > 0);
        Assert.assertEquals("JSON should have 17 nodes", countNode, 17);
        Assert.assertTrue("node 'filter' should have tool link to ubuntu", strings.get(0).contains("\"name\":\"filter\",\"id\":\"2\",\"tool\":\"https://hub.docker.com/_/ubuntu\""));
    }
}
