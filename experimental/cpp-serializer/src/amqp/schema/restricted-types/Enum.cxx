#include "Enum.h"

#include <algorithm>

#include "debug.h"

#include "Map.h"
#include "List.h"
#include "Composite.h"

/******************************************************************************/

amqp::internal::schema::
Enum::Enum (
        uPtr<Descriptor> & descriptor_,
        const std::string & name_,
        const std::string & label_,
        const std::vector<std::string> & provides_,
        const std::string & source_,
        std::vector<uPtr<Choice>> choices_
) : Restricted (
        descriptor_,
        name_,
        label_,
        provides_,
        amqp::internal::schema::Restricted::RestrictedTypes::enum_t)
    , m_enum { name_ }
    , m_choices (std::move (choices_))
{
}

/******************************************************************************/

std::vector<std::string>::const_iterator
amqp::internal::schema::
Enum::begin() const {
    return m_enum.begin();
}

/******************************************************************************/

std::vector<std::string>::const_iterator
amqp::internal::schema::
Enum::end() const {
    return m_enum.end();
}

/******************************************************************************/

int
amqp::internal::schema::
Enum::dependsOnMap (const amqp::internal::schema::Map & map_) const {
    // does lhs_ depend on us
    auto lhsMapOf { map_.mapOf() };
    if (lhsMapOf.first.get() == name() || lhsMapOf.second.get() == name()) {
        return 1;
    }

    return 0;
}

/******************************************************************************/

int
amqp::internal::schema::
Enum::dependsOnList (const amqp::internal::schema::List & list_) const {
    // does the left hand side depend on us
    if (list_.listOf() == name()) {
        return 2;
    }

    // we can never depend on the left hand side so don't bother checking

    return 0;
}

/******************************************************************************/

int
amqp::internal::schema::
Enum::dependsOnEnum (const amqp::internal::schema::Enum &) const {
    // enums should never depend on one another so just return 0;
    return 0;
}

/*********************************************************o*********************/

int
amqp::internal::schema::
Enum::dependsOnRHS (const amqp::internal::schema::Composite & lhs_) const {
    if (name() == lhs_.name()) {
        return 1;
    }

    for (const auto & field : lhs_.fields()) {
        if (field->resolvedType() == name()) {
            return 2;
        }
    }


    return 0;
}

/*********************************************************o*********************/

std::vector<std::string>
amqp::internal::schema::
Enum::makeChoices() const {
    std::vector<std::string> rtn;
    std::transform (
            m_choices.begin(),
            m_choices.end(),
            std::back_inserter(rtn),
            [](const uPtr<Choice> & c) -> std::string { return c->choice(); });

    return rtn;
}

/*********************************************************o*********************/

