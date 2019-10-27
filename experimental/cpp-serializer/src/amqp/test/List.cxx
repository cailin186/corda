#include <gtest/gtest.h>

#include "restricted-types/List.h"

/******************************************************************************/

TEST (List, name) {
    auto rtn = amqp::internal::schema::List::listType("java.util.list<int>");

    ASSERT_EQ ("java.util.list", rtn.first);
    ASSERT_EQ ("int", rtn.second);
}

/******************************************************************************/
