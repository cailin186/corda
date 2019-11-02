#include <gtest/gtest.h>

#include "Descriptor.h"
#include "restricted-types/Map.h"

/******************************************************************************
 *
 * mapType Tests
 *
 ******************************************************************************/

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

/******************************************************************************
 *
 *
 *
 ******************************************************************************/

namespace {
    std::unique_ptr<amqp::internal::schema::Map>
    map (const std::string & name_) {
        auto desc = std::make_unique<amqp::internal::schema::Descriptor> (
            "net.corda.test1");

        std::vector<std::string> provides { };

        return std::make_unique<amqp::internal::schema::Map>(
           desc,
           name_,
           "label",
           provides,
           "map"
        );
    }
}

/******************************************************************************/

/*
TEST (MAP, dependsOn1) {
    auto m = map ()
}
 */

/******************************************************************************/

