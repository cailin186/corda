#include "Enum.h"

#include <algorithm>

#include "debug.h"

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
        amqp::internal::schema::Restricted::RestrictedTypes::Enum)
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

}

/******************************************************************************/

int
amqp::internal::schema::
Enum::dependsOnList (const amqp::internal::schema::List & list_) const {
    // does the left hand side depend on us
    DBG ("  L/L a) " << list.listOf() << " == " << name() << std::endl); // NOLINT
    if (list_.listOf() == name()) {
        return 1;
    }

    // do we depend on the lhs
    DBG ("  L/L b) " << name() << " == " << list.name() << std::endl); // NOLINT
    if (name() == list_.name()) {
        return 2;
    }

    return 0;
}

/******************************************************************************/

int
amqp::internal::schema::
Enum::dependsOnEnum (const amqp::internal::schema::Enum &) const {
    // enums should never depend on one another so just return 0;
    return 0;
}

/******************************************************************************/

int
amqp::internal::schema::
Enum::dependsOn (const amqp::internal::schema::Composite & lhs_) const {
    auto rtn { 0 };
    for (const auto & field : lhs_.fields()) {
        DBG ("L/C a) " << field->resolvedType() << " == " << name() << std::endl); // NOLINT
        if (field->resolvedType() == name()) {
            rtn = 1;
        }
    }

    DBG ("  L/C b) " << name() << " == " << lhs_.name() << std::endl); // NOLINT
    if (name() == lhs_.name()) {
        rtn = 2;
    }

    return rtn;
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
