using System;
using System.Net.Security;
using NUnit.Framework;

namespace RandomCodeOrg.NetMaven.TestProject{


	public class Test {
	
		[Test]
        public void SomeTest() {
            Console.WriteLine("This is a test!");
            MainClass.Add(10,20);
        }

	}
}