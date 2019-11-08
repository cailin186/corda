#pragma once

#include "Restricted.h"

/******************************************************************************
 *
 * Forward class declarations
 *
 ******************************************************************************/

namespace amqp::internal::schema {

    class Map;
    class List;

}

/******************************************************************************/

namespace amqp::internal::schema {

    class Enum : public Restricted {
        private :
            std::vector<std::string> m_enum;
            std::vector<uPtr<Choice>> m_choices;

            int dependsOnMap (const class Map &) const override;
            int dependsOnList (const class List &) const override;
            int dependsOnEnum (const Enum &) const override;

        public :
            Enum (
                uPtr<Descriptor> & descriptor_,
                const std::string &,
                const std::string &,
                const std::vector<std::string> &,
                const std::string &,
                std::vector<uPtr<Choice>>);

            std::vector<std::string>::const_iterator begin() const override;
            std::vector<std::string>::const_iterator end() const override;

            int dependsOn (const class Composite &) const override;
            int dependsOn (const class Restricted &) const override;

            std::vector<std::string> makeChoices() const;
    };

}

/******************************************************************************/
