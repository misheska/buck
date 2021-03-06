/*
 * Copyright 2012-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.cli;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.facebook.buck.testutil.integration.DebuggableTemporaryFolder;
import com.facebook.buck.testutil.integration.ProjectWorkspace;
import com.facebook.buck.testutil.integration.ProjectWorkspace.ProcessResult;
import com.facebook.buck.testutil.integration.TestDataHelper;
import com.google.common.base.Joiner;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

/**
 * Integration test for the {@code buck project} command.
 */
public class ProjectIntegrationTest {

  @Rule
  public DebuggableTemporaryFolder temporaryFolder = new DebuggableTemporaryFolder();

  @Test
  public void testBuckProject() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "project1", temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand("project");
    result.assertSuccess("buck project should exit cleanly");

    workspace.verify();

    assertEquals(
        "`buck project` should report the files it modified.",
        Joiner.on('\n').join(
          "MODIFIED FILES:",
          ".idea/compiler.xml",
          ".idea/libraries/__libs:generated_jar.xml",
          ".idea/libraries/libs_guava_jar.xml",
          ".idea/libraries/libs_jsr305_jar.xml",
          ".idea/libraries/libs_junit_jar.xml",
          ".idea/modules.xml",
          ".idea/runConfigurations/Debug_Buck_test.xml",
          "modules/dep1/module_modules_dep1.iml",
          "modules/tip/module_modules_tip.iml",
          "root.iml"
        ) + '\n',
        result.getStdout());

    assertThat(
        "`buck project` should contain warning to restart IntelliJ.",
        result.getStderr(),
        containsString("  ::  Please close and re-open IntelliJ."));
  }

  @Test
  public void testBuckProjectExcludesSubdirectories() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "project2", temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand("project");
    result.assertSuccess("buck project should exit cleanly");

    workspace.verify();
  }
  /**
   * Verify that if we build a project by specifying a target, the resulting project only contains
   * the transitive deps of that target.  In this example, that means everything except
   * //modules/tip and //tests/tests.
   */
  @Test
  public void testBuckProjectSlice() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "project_slice", temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand("project", "//modules/dep1:dep1", "//:root");
    result.assertSuccess("buck project should exit cleanly");

    workspace.verify();

    assertEquals(
        "`buck project` should report the files it modified.",
        Joiner.on('\n').join(
            "MODIFIED FILES:",
            ".idea/compiler.xml",
            ".idea/libraries/libs_guava_jar.xml",
            ".idea/libraries/libs_jsr305_jar.xml",
            ".idea/libraries/libs_junit_jar.xml",
            ".idea/modules.xml",
            ".idea/runConfigurations/Debug_Buck_test.xml",
            "module_.iml",
            "modules/dep1/module_modules_dep1.iml"
        ) + '\n',
        result.getStdout());

    assertThat(
        "`buck project` should contain warning to restart IntelliJ.",
        result.getStderr(),
        containsString("  ::  Please close and re-open IntelliJ."));
  }

  /**
   * Verify we can build a project by specifying a target, even if it depends on a target whose
   * project is not in the same buck file as the targets it's for.
   */
  @Test
  public void testBuckProjectSliceWithProjectInDifferentBuckFile() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "project_slice_with_project_in_different_buck_file", temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand("project", "//:root");
    result.assertSuccess("buck project should exit cleanly");

    workspace.verify();

    assertEquals(
        "`buck project` should report the files it modified.",
        Joiner.on('\n').join(
            "MODIFIED FILES:",
            ".idea/compiler.xml",
            ".idea/modules.xml",
            ".idea/runConfigurations/Debug_Buck_test.xml",
            "module_.iml",
            "modules/module_modules_dep1.iml"
        ) + '\n',
        result.getStdout());

    assertThat(
        "`buck project` should contain warning to restart IntelliJ.",
        result.getStderr(),
        containsString("  ::  Please close and re-open IntelliJ."));
  }

  /**
   * Verify that if we build a project by specifying a target and '--with-tests', the resulting
   * project only contains the transitive deps of that target as well as any tests that specify
   * something in those transitive deps as "sources_under_test".  In this example, that means
   * everything except //modules/tip.
   */
  @Test
  public void testBuckProjectSliceWithTests() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "project_slice_with_tests", temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--with-tests",
        "//modules/dep1:dep1");
    result.assertSuccess("buck project should exit cleanly");

    workspace.verify();

    assertEquals(
        "`buck project` should report the files it modified.",
        Joiner.on('\n').join(
            "MODIFIED FILES:",
            ".idea/compiler.xml",
            ".idea/libraries/libs_guava_jar.xml",
            ".idea/libraries/libs_jsr305_jar.xml",
            ".idea/libraries/libs_junit_jar.xml",
            ".idea/modules.xml",
            ".idea/runConfigurations/Debug_Buck_test.xml",
            "modules/dep1/module_modules_dep1.iml",
            "tests/module_tests.iml"
        ) + '\n',
        result.getStdout());

    assertThat(
        "`buck project` should contain warning to restart IntelliJ.",
        result.getStderr(),
        containsString("  ::  Please close and re-open IntelliJ."));
  }

  /**
   * Verify that if we build a project by specifying a target and '--with-tests', the tests'
   * dependencies are referenced even if they are defined in a buck file that would not have been
   * parsed otherwise.
   */
  @Test
  public void testBuckProjectSliceWithTestsDependenciesInDifferentBuckFile() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "project_slice_with_tests_dependencies_in_different_buck_file", temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--with-tests",
        "//modules/dep1:dep1");
    result.assertSuccess("buck project should exit cleanly");

    workspace.verify();

    assertEquals(
        "`buck project` should report the files it modified.",
        Joiner.on('\n').join(
            "MODIFIED FILES:",
            ".idea/compiler.xml",
            ".idea/modules.xml",
            ".idea/runConfigurations/Debug_Buck_test.xml",
            "modules/dep1/module_modules_dep1.iml",
            "modules/dep2/module_modules_dep2.iml",
            "tests/module_tests.iml"
        ) + '\n',
        result.getStdout());

    assertThat(
        "`buck project` should contain warning to restart IntelliJ.",
        result.getStderr(),
        containsString("  ::  Please close and re-open IntelliJ."));
  }

  /**
   * Verify that if we build a project by specifying a target and '--with-tests', the tests'
   * projects rules are referenced even if they are defined in a different buck file from the tests.
   */
  @Test
  public void testBuckProjectSliceWithTestsProjectInDifferentBuckFile() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "project_slice_with_tests_project_in_different_buck_file", temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--with-tests",
        "//modules/dep1:dep1");
    result.assertSuccess("buck project should exit cleanly");

    workspace.verify();

    assertEquals(
        "`buck project` should report the files it modified.",
        Joiner.on('\n').join(
            "MODIFIED FILES:",
            ".idea/compiler.xml",
            ".idea/modules.xml",
            ".idea/runConfigurations/Debug_Buck_test.xml",
            "modules/dep1/module_modules_dep1.iml",
            "tests/module_tests_test1.iml"
        ) + '\n',
        result.getStdout());

    assertThat(
        "`buck project` should contain warning to restart IntelliJ.",
        result.getStderr(),
        containsString("  ::  Please close and re-open IntelliJ."));
  }

  /**
   * Tests the case where a build file has a test rule that depends on a library rule in the same
   * build file, and the test rule is specified as the {@code test_target} in its
   * {@code project_config()}. When this happens, all libraries in the generated {@code .iml} file
   * should be listed before any of the modules.
   * <p>
   * This prevents a regression where JUnit was not being loaded early enough in the classpath,
   * which led to a "JUnit version 3.8 or later expected" error when running tests in IntelliJ.
   * (Presumably, IntelliJ ended up loading JUnit 3 from android.jar instead of loading JUnit 4
   * from the version of JUnit checked into version control.)
   */
  @Test
  public void testBuckProjectWithMultipleLibrariesInOneBuildFile() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this, "buck_project_multiple_libraries_in_one_build_file", temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand("project");
    result.assertSuccess("buck project should exit cleanly");

    workspace.verify();
  }

  @Test
  public void buckProjectXcodeSeparatedProjectsNoArgsGeneratesAllProjects() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "project_xcode_two_projects",
        temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand("project");
    result.assertSuccess();

    workspace.verify();
  }

  @Test
  public void testBuckProjectGeneratedSchemeOnlyIncludesDependencies() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "project_generated_scheme_only_includes_dependencies",
        temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--workspace-and-projects",
        "//Apps:workspace");
    result.assertSuccess();

    workspace.verify();
  }

  @Test
  public void testBuckProjectGeneratedSchemeIncludesTestsAndDependencies() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "project_generated_scheme_includes_tests_and_dependencies",
        temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--workspace-and-projects",
        "--with-tests",
        "//Apps:workspace");
    result.assertSuccess();

    workspace.verify();
  }

  @Test
  public void testBuckProjectGeneratedSchemeIncludesTestsAndDependenciesInADifferentBuckFile()
      throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "project_generated_scheme_includes_tests_and_dependencies_in_a_different_buck_file",
        temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--workspace-and-projects",
        "--with-tests",
        "//Apps:workspace");
    result.assertSuccess();

    workspace.verify();
  }

  @Test
  public void testBuckProjectGeneratedSchemesDoNotIncludeOtherTests()
      throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "project_generated_schemes_do_not_include_other_tests",
        temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--workspace-and-projects",
        "--with-tests");
    result.assertSuccess();

    workspace.verify();
  }

  @Test
  public void projectRulesCanReferenceTargetsInOtherBuckFiles() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "project_rules_can_reference_targets_in_other_buck_files",
        temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--workspace-and-projects");
    result.assertSuccess();

    workspace.verify();
  }

  @Test
  public void allTargetsInAProjectRuleCanBeInOtherBuckFiles() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "all_targets_in_a_project_rule_can_be_in_other_buck_files",
        temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--workspace-and-projects");
    result.assertSuccess();

    workspace.verify();
  }

  @Test
  public void generatingAllWorkspacesWillNotIncludeAllProjectsInEachOfThem() throws IOException {
    ProjectWorkspace workspace = TestDataHelper.createProjectWorkspaceForScenario(
        this,
        "generating_all_workspaces_will_not_include_all_projects_in_each_of_them",
        temporaryFolder);
    workspace.setUp();

    ProcessResult result = workspace.runBuckCommand(
        "project",
        "--workspace-and-projects");
    result.assertSuccess();

    workspace.verify();
  }
}
