#pragma once
#include "COpenSSLAsymmetricKeysGenerator.h"
namespace YSecurity
{
	class COpenSSLRSAEncryptAsymmetricKeysGenerator :
		public COpenSSLAsymmetricKeysGenerator
	{

	private:
		void freeKey();
	public:

		COpenSSLRSAEncryptAsymmetricKeysGenerator(int lenKey);
		~COpenSSLRSAEncryptAsymmetricKeysGenerator();
		void setAlgoritm(int id) = delete;


		bool checkKeys() override;

		void generate();

		void setLenKey(int len);
	};

}