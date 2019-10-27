#include <gtest/gtest.h>

#include "restricted-types/Map.h"

/******************************************************************************/

TEST (Map, name1) {
    auto [map, of, to] = amqp::internal::schema::Map::mapType (
            "java.util.Map<int, string>");

    ASSERT_EQ("java.util.Map", map);
    ASSERT_EQ("int", of);
    ASSERT_EQ("string", to);
}

/******************************************************************************/

TEST (Map, name2) {
    auto [map, of, to] = amqp::internal::schema::Map::mapType (
            "java.util.Map<int, java.util.List<string>>");

    ASSERT_EQ("java.util.Map", map);
    ASSERT_EQ("int", of);
    ASSERT_EQ("java.util.List<string>", to);
}

/******************************************************************************/

TEST (Map, name3) {
    auto [map, of, to] = amqp::internal::schema::Map::mapType (
            "java.util.Map<java.util.Pair<int, int>, java.util.List<string>>");

    ASSERT_EQ("java.util.Map", map);
    ASSERT_EQ("java.util.Pair<int, int>", of);
    ASSERT_EQ("java.util.List<string>", to);
}

/******************************************************************************/
