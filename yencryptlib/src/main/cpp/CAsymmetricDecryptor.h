#pragma once
#include "CDecryptor.h"
namespace YSecurity
{
	class CAsymmetricDecryptor :
		public CDecryptor
	{
	public:
		CAsymmetricDecryptor();
		virtual ~CAsymmetricDecryptor();
		virtual void setAsymmetricKey(unsigned char * key, int lenKey) = 0;
	};
}