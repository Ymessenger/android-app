#pragma once
#include "CGenerator.h"
namespace YSecurity
{
	class CAsymmetricKeysGenerator : public CGenerator
	{
	protected:

	public:


		CAsymmetricKeysGenerator();
		virtual ~CAsymmetricKeysGenerator();


		virtual unsigned char * getPublicKey() = 0;
		virtual unsigned char * getPrivateKey() = 0;

		virtual int getLenBufferPublicKey() = 0;
		virtual int getLenBufferPrivateKey() = 0;

	};
}

