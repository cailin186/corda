#pragma once

#include "Restricted.h"

/******************************************************************************/

namespace amqp::internal::schema {

    class Enum : public Restricted {
        private :
            std::vector<std::string> m_enum;
            std::vector<uPtr<Choice>> m_choices;

            int dependsOnMap (const Map &) const override;
            int dependsOnList (const List &) const override;
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

            int dependsOnRHS (const Composite &) const override;

            std::vector<std::string> makeChoices() const;
    };

}

/******************************************************************************/
