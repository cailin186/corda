#include "TestUtils.h"

#include <algorithm>
#include <string>

#include "restricted-types/Map.h"

/******************************************************************************/

namespace {

    std::string fingerprint() {
        auto randchar = []() -> char {
            const char charset[] =
                    "0123456789"
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    "abcdefghijklmnopqrstuvwxyz";
            const size_t max_index = (sizeof(charset) - 1);
            return charset[rand() % max_index];
        };
        std::string str(20, 0);
        std::generate_n(str.begin(), 20, randchar);

        return "net.corda:" + str;
    }

}

/******************************************************************************/

std::unique_ptr<amqp::internal::schema::Map>
test::
map (const std::string & of_, const std::string & to_) {
    auto desc = std::make_unique<amqp::internal::schema::Descriptor> (
            fingerprint());

    std::vector<std::string> provides { };

    return std::make_unique<amqp::internal::schema::Map>(
            desc,
            "java.util.Map<" + of_ + ", " + to_ + ">",
            "label",
            provides,
            "map"
    );
}

/******************************************************************************/

std::unique_ptr <amqp::internal::schema::List>
test::
list (const std::string & of_) {
    auto desc = std::make_unique<amqp::internal::schema::Descriptor> (
            fingerprint());

    std::vector<std::string> provides { };

    return std::make_unique<amqp::internal::schema::List>(
            desc,
            "java.util.List<" + of_ + ">",
            "label",
            provides,
            "map"
    );
}

/******************************************************************************/
