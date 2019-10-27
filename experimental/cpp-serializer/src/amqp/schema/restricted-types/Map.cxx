#include "Map.h"

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
Map::dependsOn (const Restricted &) const  {

}

/******************************************************************************/

int
amqp::internal::schema::
Map::dependsOn (const class Composite &) const  {

}

/******************************************************************************/
