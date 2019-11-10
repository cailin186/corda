#pragma once

#include "Restricted.h"

/******************************************************************************/

namespace amqp::internal::schema {

    class List : public Restricted {
        public :
            static std::pair<std::string, std::string> listType (
                    const std::string &);

        private :
            std::vector<std::string> m_listOf;

            int dependsOnMap (const Map &) const override;
            int dependsOnList (const List &) const override;
            int dependsOnEnum (const Enum &) const override;

        public :
            List (
                uPtr<Descriptor> &,
                const std::string &,
                const std::string &,
                const std::vector<std::string> &,
                const std::string &);

            std::vector<std::string>::const_iterator begin() const override;
            std::vector<std::string>::const_iterator end() const override;

            const std::string & listOf() const;

            int dependsOnRHS (const Composite &) const override;
    };

}

/******************************************************************************/
