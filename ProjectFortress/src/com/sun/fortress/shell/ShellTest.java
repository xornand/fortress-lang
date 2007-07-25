/*******************************************************************************
    Copyright 2007 Sun Microsystems, Inc.,
    4150 Network Circle, Santa Clara, California 95054, U.S.A.
    All rights reserved.

    U.S. Government Rights - Commercial software.
    Government users are subject to the Sun Microsystems, Inc. standard
    license agreement and applicable provisions of the FAR and its supplements.

    Use is subject to license terms.

    This distribution may include materials developed by third parties.

    Sun, Sun Microsystems, the Sun logo and Java are trademarks or registered
    trademarks of Sun Microsystems, Inc. in the U.S. and other countries.
 ******************************************************************************/

package com.sun.fortress.shell;

import java.io.File;
import junit.framework.TestCase;
// import org.apache.tools.ant.launch.Launcher;

/**
 * A JUnit test case class.
 * Every method starting with the word "test" will be called when running
 * the test with JUnit.
 */
public class ShellTest extends TestCase {

   public void testSelfUpgrade() throws UserError, InterruptedException {
      Shell shell = new Shell();
      shell.selfUpgrade("fortress_mock_upgrade.jar");
      assert(new File(".java/fortress_mock_upgrade.jar").exists());
   }

   public void testCompile() throws UserError, InterruptedException {
      //assert(! Component.exists("TestComponent.fss"));
      Shell shell = new Shell();
      shell.compile("TestComponent.fss");
      //assert(Component.exists("TestComponent.fss"));
   }


}
