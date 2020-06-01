#include "CError.h"

namespace YSecurity
{

	CError::CError()
	{
	}


	CError::~CError()
	{
	}


	int CError::getCodeLastError()
	{
		return this->codeError;
	}

	bool CError::isError()
	{
		return this->_isError;
	}

	std::string CError::getLastError()
	{
		return this->lastError;
	}

}