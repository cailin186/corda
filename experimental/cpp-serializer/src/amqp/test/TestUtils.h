#pragma once

#include <string>
#include "restricted-types/List.h"
#include "restricted-types/Map.h"

/******************************************************************************/

namespace test {
    uPtr <amqp::internal::schema::Map>
    map (const std::string &, const std::string &);

    uPtr <amqp::internal::schema::List>
    list (const std::string & of_);

    uPtr <amqp::internal::schema::Enum>
    eNum (const std::string & e_);
}

/******************************************************************************/
