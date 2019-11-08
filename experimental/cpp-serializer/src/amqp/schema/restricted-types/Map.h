#pragma once

#include "Restricted.h"

/******************************************************************************
 *
 * Forward class declarations
 *
 ******************************************************************************/

namespace amqp::internal::schema {

    class List;
    class Enum;

}

/******************************************************************************/

namespace amqp::internal::schema {

    class Map : public Restricted {
        public :
            static std::tuple<std::string, std::string, std::string>
            mapType (const std::string &);

        private :
            std::vector<std::string> m_mapOf;

            int dependsOnMap (const Map &) const override;
            int dependsOnList (const class List &) const override;
            int dependsOnEnum (const class Enum &) const override;

        public :
            Map (
                uPtr<Descriptor> & descriptor_,
                const std::string &,
                const std::string &,
                const std::vector<std::string> &,
                const std::string &);

            std::vector<std::string>::const_iterator begin() const override;
            std::vector<std::string>::const_iterator end() const override;

            std::pair<
                std::reference_wrapper<const std::string>,
                std::reference_wrapper<const std::string>> mapOf() const;

            int dependsOn (const class Composite &) const override;
            int dependsOn (const class Restricted &) const override;
    };

}

/******************************************************************************/
