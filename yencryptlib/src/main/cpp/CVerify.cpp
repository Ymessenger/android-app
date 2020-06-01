#include "CVerify.h"
#include "string.h"

namespace YSecurity
{
	CVerify::CVerify()
	{
	}

	CVerify::~CVerify()
	{
		if (sign)
			delete[] sign;
	}

	void CVerify::setSign(unsigned char * sign, size_t sLen)
	{
		if (this->sign)
			delete[] this->sign;
		if (sign && sLen > 0)
		{
			this->lenSign = sLen;
			this->sign = new unsigned char[sLen + 1];;
			memset(this->sign, 0, sLen + 1);
			memcpy(this->sign, sign, sLen);
		}
		else
		{
			this->_isError = true;
			this->codeError = 5001;
			this->lastError = "Error sign";
		}
	}

	bool CVerify::isCorrect()
	{
		if (!this->_isError && isVerify)
			return correct;
		else
			return false;
	}
}
