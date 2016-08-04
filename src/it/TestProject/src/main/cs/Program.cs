using System;
using System.Net.Security;


namespace RandomCodeOrg.NetMaven.TestProject{
	class MainClass {
	
		public static void Main(string[] args){
			Console.WriteLine("Hello World!");
			System.Security.Cryptography.AesManaged aes = new System.Security.Cryptography.AesManaged();
			System.Security.Cryptography.Xml.EncryptedReference eer = null;
			Console.WriteLine("Muu");
		}
		
	}
}