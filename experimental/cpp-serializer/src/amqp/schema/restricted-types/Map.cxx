#include "Map.h"
#include "List.h"
#include "Enum.h"
#include "Composite.h"

/******************************************************************************
 *
 * Map Static Member Functions
 *
 ******************************************************************************/

std::tuple<std::string, std::string, std::string>
amqp::internal::schema::
Map::mapType (const std::string & map_) {
    uint pos = map_.find ('<');

    uint idx { pos + 1 };
    for (uint nesting { 0 } ; idx < map_.size(); ++idx) {
        if (map_[idx] == '<') {
            ++nesting;
        } else if (map_[idx] == '>') {
            --nesting;
        } else if (map_[idx] == ',' && nesting == 0) {
            break;
        }
    }

    auto map = std::string { map_.substr (0, pos) };
    auto of  = std::string { map_.substr (pos + 1, idx - pos - 1) };
    of = of.erase(0, of.find_first_not_of(' '));
    auto to  = std::string { map_.substr (idx + 1, map_.size() - (idx + 2)) };
    to = to.erase(0, to.find_first_not_of(' '));

    return { map, of, to };
}

/******************************************************************************
 *
 * Map Member Functions
 *
 ******************************************************************************/

amqp::internal::schema::
Map::Map (
    uPtr<Descriptor> & descriptor_,
    const std::string & name_,
    const std::string & label_,
    const std::vector<std::string> & provides_,
    const std::string & source_
) : Restricted (
        descriptor_,
        name_,
        label_,
        provides_,
        amqp::internal::schema::Restricted::RestrictedTypes::Map)
{
    auto [map, of, to] = mapType (name_);
    m_mapOf = { of, to };
}

/******************************************************************************/

std::vector<std::string>::const_iterator
amqp::internal::schema::
Map::begin() const {
    return m_mapOf.begin();
}

/******************************************************************************/

std::vector<std::string>::const_iterator
amqp::internal::schema::
Map::end() const  {
    return m_mapOf.end();
}

/******************************************************************************/

std::pair<
    std::reference_wrapper<const std::string>,
    std::reference_wrapper<const std::string>>
amqp::internal::schema::
Map::mapOf() const {
    return std::pair { std::cref (m_mapOf[0]), std::cref (m_mapOf[1]) };
}

/******************************************************************************/

int
amqp::internal::schema::
Map::dependsOnMap (const amqp::internal::schema::Map & lhs_) const {
    // does lhs_ depend on us
    auto lhsMapOf { lhs_.mapOf() };
    if (lhsMapOf.first.get() == name() || lhsMapOf.second.get() == name()) {
        return 1;
    }

    // do we depend on the lhs
    if (m_mapOf[0] == lhs_.name() || m_mapOf[1] == lhs_.name()) {
        return 2;
    }

    return 0;
}

/******************************************************************************/

int
amqp::internal::schema::
Map::dependsOnList (const amqp::internal::schema::List & lhs_) const {
    // does lhs_ depend on us
    if (lhs_.listOf() == name()) {
        return 1;
    }

    // do we depend on the lhs
    if (m_mapOf[0] == lhs_.name() || m_mapOf[1] == lhs_.name()) {
        return 2;
    }

    return 0;
}

/******************************************************************************/

int
amqp::internal::schema::
Map::dependsOnEnum (const amqp::internal::schema::Enum & lhs_) const {
    // does lhs_ depend on us
    if (lhs_.name() == name()) {
        return 1;
    }

    // do we depend on the lhs
    if (m_mapOf[0] == lhs_.name() || m_mapOf[1] == lhs_.name()) {
        return 2;
    }

    return 0;
}

/******************************************************************************/

int
amqp::internal::schema::
Map::dependsOn (const amqp::internal::schema::Composite & lhs_) const  {
    auto rtn { 0 };

    // does lhs_ depend on us
    for (const auto & field : lhs_.fields()) {
        if (field->resolvedType() == name()) {
            rtn = 1;
        }
    }

    // do we depend on the lhs
    if (m_mapOf[0] == lhs_.name() || m_mapOf[1] == lhs_.name()) {
        rtn = 2;
    }

    return rtn;
}

/******************************************************************************/

int
amqp::internal::schema::
Map::dependsOn (const Restricted & lhs_) const {
    return Restricted::dependsOn (lhs_);
}

/******************************************************************************/
