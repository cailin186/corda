#pragma once

#include <string>
#include "restricted-types/List.h"
#include "restricted-types/Map.h"

/******************************************************************************/

namespace test {
    std::unique_ptr <amqp::internal::schema::Map>
    map (const std::string &, const std::string &);

    std::unique_ptr <amqp::internal::schema::List>
    list (const std::string & of_);
}

/******************************************************************************/
